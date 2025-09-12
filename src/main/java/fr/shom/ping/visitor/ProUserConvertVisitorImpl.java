package fr.shom.ping.visitor;

import fr.shom.ping.model.FunctionalAdmin;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.ServiceUser;

public class ProUserConvertVisitorImpl implements ProUserConvertVisitor<ProUser> {

    @Override
    public ProUser toFunctionalAdmin(ServiceUser serviceUser) {
        return serviceUser.toFunctionalAdmin();
    }

    @Override
    public ProUser toServiceUser(FunctionalAdmin functionalAdmin) {
        return functionalAdmin.toServiceUser();
    }
}
