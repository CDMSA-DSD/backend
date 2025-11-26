package dsd.api.cdmsa.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import dsd.api.cdmsa.controller.OrganizationInvitationController;
import dsd.api.cdmsa.dto.InvitationResponse;
import dsd.api.cdmsa.mapper.InvitationMapper;
import dsd.api.cdmsa.model.OrganizationInvitation;


@Component
public class InvitationModelAssembler implements RepresentationModelAssembler<OrganizationInvitation, EntityModel<InvitationResponse>> {

    @Override
    @NonNull
    public EntityModel<InvitationResponse> toModel(OrganizationInvitation invitation) {
        InvitationResponse dto = InvitationMapper.toDto(invitation);

        return EntityModel.of(dto,
                // self link: GET /invitation/{id}
                linkTo(methodOn(OrganizationInvitationController.class).getInvitation(invitation.getId())).withSelfRel());

    }
}
