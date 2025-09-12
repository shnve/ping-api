package fr.shom.ping.visitor;

import fr.shom.ping.model.FunctionalAdmin;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.dto.FunctionalAdminEditDto;
import fr.shom.ping.model.dto.ProUserEditDto;
import fr.shom.ping.model.dto.ServiceUserEditDto;

public class ProUserEditDtoVisitorImpl implements ProUserEditDtoVisitor<ProUserEditDto> {

    @Override
    public ProUserEditDto toServiceUserEditDto(ServiceUser serviceUser) {
        return new ServiceUserEditDto(serviceUser);
    }

    @Override
    public ProUserEditDto toFunctionalAdminEditDto(FunctionalAdmin functionalAdmin) {
        return new FunctionalAdminEditDto(functionalAdmin);
    }
}
