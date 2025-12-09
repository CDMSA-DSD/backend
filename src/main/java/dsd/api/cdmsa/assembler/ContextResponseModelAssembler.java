package dsd.api.cdmsa.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import dsd.api.cdmsa.controller.ContextController;
import dsd.api.cdmsa.dto.ContextResponse;
import dsd.api.cdmsa.model.Context;

@Component
public class ContextResponseModelAssembler
        implements RepresentationModelAssembler<Context, EntityModel<ContextResponse>> {

    @Override
    @NonNull
    public EntityModel<ContextResponse> toModel(Context context) {
        ContextResponse dto = ContextResponse.fromEntity(context);

        return EntityModel.of(dto,
                // self link: GET /users/{id}
                linkTo(methodOn(ContextController.class).getContext(null, context.getId())).withSelfRel());
    }

}
