package fr.shom.ping.visitor;

import fr.shom.ping.model.FunctionalAdmin;
import fr.shom.ping.model.ServiceUser;

public interface ProUserConvertVisitor<T> {
    T toFunctionalAdmin(ServiceUser serviceUser);

    T toServiceUser(FunctionalAdmin functionalAdmin);
}
