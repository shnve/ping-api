package fr.shom.ping.repository;

import fr.shom.ping.model.Tag;
import fr.shom.ping.model.dto.TagDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, UUID> {


    @Query("SELECT new fr.shom.ping.model.dto.TagDto(tag) FROM Tag tag WHERE tag.value = :value AND tag.maritimeService.id = :maritimeServiceId")
    List<TagDto> findByValue(String value, UUID maritimeServiceId);

    @Modifying
    @Query("DELETE FROM Tag tag WHERE tag.id = :id")
    void delete(UUID id);
}
