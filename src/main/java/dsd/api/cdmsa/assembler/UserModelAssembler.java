package dsd.api.cdmsa.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import dsd.api.cdmsa.controller.UserController;
import dsd.api.cdmsa.dto.UserResponse;
import dsd.api.cdmsa.mapper.UserMapper;
import dsd.api.cdmsa.model.User;

@Component
public class UserModelAssembler implements RepresentationModelAssembler<User, EntityModel<UserResponse>> {

    @Override
    @NonNull
    public EntityModel<UserResponse> toModel(User user) {
        UserResponse dto = UserMapper.toDto(user);

        return EntityModel.of(dto,
                // self link: GET /users/{id}
                linkTo(methodOn(UserController.class).getUser(user.getId())).withSelfRel());

    }
}
