package fr.shom.ping.repository;

import fr.shom.ping.model.Attachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findAllByIdIn(List<UUID> ids);

    /** PISAC (S-7): comprueba si el adjunto pertenece a una contribución o levantamiento del usuario público. */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM infosource_attachment ia
            JOIN infosource i ON i.id = ia.infosource_id
            JOIN public_user pu ON pu.id = i.issuing_public_user_id
            WHERE ia.attachment_id = :attachmentId AND lower(pu.email) = lower(:email)
            UNION ALL
            SELECT 1 FROM survey s
            JOIN public_user pu ON pu.id = s.issuing_public_user_id
            WHERE s.technical_metadata_file_id = :attachmentId AND lower(pu.email) = lower(:email)
            UNION ALL
            SELECT 1 FROM survey_attachment sa
            JOIN survey s ON s.id = sa.survey_id
            JOIN public_user pu ON pu.id = s.issuing_public_user_id
            WHERE sa.attachment_id = :attachmentId AND lower(pu.email) = lower(:email)
            UNION ALL
            SELECT 1 FROM survey_file sf
            JOIN survey_file_list sfl ON sfl.survey_file_id = sf.id
            JOIN survey s ON s.id = sfl.survey_id
            JOIN public_user pu ON pu.id = s.issuing_public_user_id
            WHERE sf.attachment_id = :attachmentId AND lower(pu.email) = lower(:email)
        )
        """, nativeQuery = true)
    boolean isOwnedByPublicUser(@Param("attachmentId") UUID attachmentId, @Param("email") String email);

    /** PISAC (S-7): comprueba si el adjunto ya está asociado a algún registro del sistema. */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM warning_attachment WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM georegulation_attachment WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM folder_attachment WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM maritime_entity_reference WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM static_file WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM infosource_attachment WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM survey WHERE technical_metadata_file_id = :attachmentId
            UNION ALL SELECT 1 FROM survey_attachment WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM survey_file WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM survey_processing_result_attachments WHERE attachment_id = :attachmentId
        )
        """, nativeQuery = true)
    boolean isLinkedToAnyRecord(@Param("attachmentId") UUID attachmentId);

    /** PISAC (S-7): un usuario público solo puede borrar un adjunto suyo que esté en borrador (o no vinculado). */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM infosource_attachment ia
            JOIN infosource i ON i.id = ia.infosource_id
            JOIN public_user pu ON pu.id = i.issuing_public_user_id
            WHERE ia.attachment_id = :attachmentId
              AND lower(pu.email) = lower(:email)
              AND i.status = 'DRAFT'
        )
        AND NOT EXISTS (
            SELECT 1 FROM warning_attachment WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM georegulation_attachment WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM folder_attachment WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM maritime_entity_reference WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM static_file WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM survey WHERE technical_metadata_file_id = :attachmentId
            UNION ALL SELECT 1 FROM survey_attachment WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM survey_file WHERE attachment_id = :attachmentId
            UNION ALL SELECT 1 FROM survey_processing_result_attachments WHERE attachment_id = :attachmentId
        )
        """, nativeQuery = true)
    boolean isDeletableInDraftInfosourceByPublicUser(@Param("attachmentId") UUID attachmentId, @Param("email") String email);

    /** PISAC (S-7): comprueba si el adjunto pertenece a un archivo estático (solo modificable por functional-admin). */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM static_file WHERE attachment_id = :attachmentId)", nativeQuery = true)
    boolean isLinkedToStaticFile(@Param("attachmentId") UUID attachmentId);

    /** PISAC (S-7): comprueba si el usuario institucional posee al menos un rol de escritura (ADMIN u OPERATOR). */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM ServiceUser u JOIN u.roles r "
        + "WHERE lower(u.email) = lower(:email) "
        + "AND r.serviceRole IN (fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN, fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR)")
    boolean hasServiceUserWriteRole(@Param("email") String email);
}
