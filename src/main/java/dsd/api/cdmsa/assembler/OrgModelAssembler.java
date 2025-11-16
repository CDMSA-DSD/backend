package dsd.api.cdmsa.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import dsd.api.cdmsa.controller.OrgController;
import dsd.api.cdmsa.controller.UserController;
import dsd.api.cdmsa.dto.OrganizationResponse;
import dsd.api.cdmsa.mapper.OrgMapper;
import dsd.api.cdmsa.model.Organization;

@Component
public class OrgModelAssembler
        implements RepresentationModelAssembler<Organization, EntityModel<OrganizationResponse>> {

    @Override
    @NonNull
    public EntityModel<OrganizationResponse> toModel(Organization org) {

        OrganizationResponse dto = OrgMapper.toDto(org);

        EntityModel<OrganizationResponse> model = EntityModel.of(dto,
                // self link: GET /orgs/{id}
                linkTo(methodOn(OrgController.class).getOrg(org.getId())).withSelfRel());

        // rel link: GET /users/{id}
        model.add(
                linkTo(methodOn(UserController.class)
                        .getUser(org.getAdminUser().getId()))
                        .withRel("admin"));
        return model;
    }
}
