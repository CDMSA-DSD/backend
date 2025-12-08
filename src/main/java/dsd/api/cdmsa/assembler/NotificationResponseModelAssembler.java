package dsd.api.cdmsa.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import dsd.api.cdmsa.controller.RfcController;
import dsd.api.cdmsa.dto.NotificationResponse;
import dsd.api.cdmsa.model.Notification;

@Component
public class NotificationResponseModelAssembler
        implements RepresentationModelAssembler<Notification, EntityModel<NotificationResponse>> {

    @Override
    @NonNull
    public EntityModel<NotificationResponse> toModel(Notification entity) {
        NotificationResponse dto = NotificationResponse.from(entity);
        return EntityModel.of(dto,
                linkTo(methodOn(RfcController.class).getRfcById(dto.rfcId(), null))
                        .withRel("RFC"));

    }

}
