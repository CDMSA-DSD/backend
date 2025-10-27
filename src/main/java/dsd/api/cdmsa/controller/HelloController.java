package dsd.api.cdmsa.controller;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  @GetMapping("/hello")
  public RepresentationModel<?> hello() {
    var model = new RepresentationModel<>();
    model.add(Link.of("/hello").withSelfRel());
    return model;
  }
}
