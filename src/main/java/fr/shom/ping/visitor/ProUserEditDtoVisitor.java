package fr.shom.ping.visitor;

import fr.shom.ping.model.FunctionalAdmin;
import fr.shom.ping.model.ServiceUser;

public interface ProUserEditDtoVisitor<T> {
    T toServiceUserEditDto(ServiceUser serviceUser);

    T toFunctionalAdminEditDto(FunctionalAdmin functionalAdmin);
}
