package support;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.Applicant;
import models.Program;
import models.Question;
import play.inject.Injector;
import services.Path;
import services.program.ProgramDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionService;
import services.question.TextQuestionDefinition;

public class ResourceCreator {

  private final QuestionService questionService;

  public ResourceCreator(Injector injector) {
    this.questionService = injector.instanceOf(QuestionService.class);
  }

  public Question insertQuestion(String pathString) {
    return insertQuestion(pathString, 1L);
  }

  public Question insertQuestion(String pathString, long version) {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            version, "", Path.create(pathString), "", ImmutableMap.of(), ImmutableMap.of());
    Question question = new Question(definition);
    question.save();
    return question;
  }

  public QuestionDefinition insertQuestionDefinition() {
    return questionService
        .create(
            new TextQuestionDefinition(
                1L,
                "question name",
                Path.create("applicant.my.path.name"),
                "description",
                ImmutableMap.of(Locale.US, "question?"),
                ImmutableMap.of(Locale.US, "help text")))
        .getResult();
  }

  public Program insertProgram(String name) {
    return ProgramBuilder.newProgram(name, "description").build();
  }

  public ProgramDefinition insertProgramWithOneBlock(String name) {
    return ProgramBuilder.newProgram(name, "desc")
        .withBlock("Block 1")
        .withQuestionDefinition(insertQuestionDefinition())
        .buildDefinition();
  }

  public Applicant insertApplicant() {
    Applicant applicant = new Applicant();
    applicant.save();
    return applicant;
  }
}
