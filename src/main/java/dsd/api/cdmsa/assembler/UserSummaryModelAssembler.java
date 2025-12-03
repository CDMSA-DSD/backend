package dsd.api.cdmsa.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import dsd.api.cdmsa.controller.UserController;
import dsd.api.cdmsa.dto.UserSummaryResponse;
import dsd.api.cdmsa.model.User;

@Component
public class UserSummaryModelAssembler implements RepresentationModelAssembler<User, EntityModel<UserSummaryResponse>> {

    @Override
    @NonNull
    public EntityModel<UserSummaryResponse> toModel(User user) {
        UserSummaryResponse dto = UserSummaryResponse.fromEntity(user);

        return EntityModel.of(dto,
                // self link: GET /users/{id}
                linkTo(methodOn(UserController.class).getUser(user.getId())).withSelfRel());

    }

    
}
