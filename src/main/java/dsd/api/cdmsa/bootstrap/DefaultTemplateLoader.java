package dsd.api.cdmsa.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import dsd.api.cdmsa.model.Template;
import dsd.api.cdmsa.repository.TemplateRepository;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DefaultTemplateLoader implements ApplicationRunner {

    private final TemplateRepository repository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String defaultName = "DEFAULT_NAME";

        if (!repository.existsByName(defaultName)) {
            Template template = new Template();
            template.setName(defaultName);
            template.setDescription("This is the template created by default");

            repository.save(template);
        }
    }

}
