package views.admin.questions;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import forms.QuestionForm;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import services.question.QuestionType;
import views.BaseHtmlView;
import views.Styles;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;

public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public QuestionEditView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content renderNewQuestionForm(Request request) {
    return layout.render(
        body(
            renderHeader("New Question"),
            buildNewQuestionForm(new QuestionForm()).with(makeCsrfTokenInputTag(request))));
  }

  public Content renderNewQuestionForm(Request request, QuestionForm questionForm, String message) {
    return layout.render(
        body(
            div(message),
            renderHeader("New Question"),
            buildNewQuestionForm(questionForm).with(makeCsrfTokenInputTag(request))));
  }

  public Content renderEditQuestionForm(Request request, QuestionDefinition question) {
    return layout.render(
        body(
            renderHeader("Edit Question"),
            buildEditQuestionForm(question.getId(), new QuestionForm(question))
                .with(makeCsrfTokenInputTag(request))));
  }

  public Content renderEditQuestionForm(
      Request request, long id, QuestionForm questionForm, String message) {
    return layout.render(
        body(
            div(message),
            renderHeader("Edit Question"),
            buildEditQuestionForm(id, questionForm).with(makeCsrfTokenInputTag(request))));
  }

  private ContainerTag buildNewQuestionForm(QuestionForm questionForm) {
    ContainerTag formTag = buildQuestionForm(questionForm);
    formTag
        .withAction(controllers.admin.routes.QuestionController.create().url())
        .with(submitButton("Create").withClass(Styles.ML_2));

    return formTag;
  }

  private ContainerTag buildEditQuestionForm(long id, QuestionForm questionForm) {
    ContainerTag formTag = buildQuestionForm(questionForm);
    formTag
        .withAction(controllers.admin.routes.QuestionController.update(id).url())
        .with(submitButton("Update").withClass(Styles.ML_2));
    return formTag;
  }

  private ContainerTag buildQuestionForm(QuestionForm questionForm) {
    ContainerTag formTag = form().withMethod("POST");
    formTag
        .with(
            FieldWithLabel.input()
                .setId("questionName")
                .setLabelText("Name")
                .setPlaceholderText("The name displayed in the question builder")
                .setValue(questionForm.getQuestionName())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("questionDescription")
                .setLabelText("Description")
                .setPlaceholderText("The description displayed in the question builder")
                .setValue(questionForm.getQuestionDescription())
                .getContainer(),
            FieldWithLabel.input()
                .setId("questionPath")
                .setLabelText("Path")
                .setPlaceholderText("The path used to store question data")
                .setValue(questionForm.getQuestionPath().path())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("questionText")
                .setLabelText("Question text")
                .setPlaceholderText("The question text displayed to the applicant")
                .setValue(questionForm.getQuestionText())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("questionHelpText")
                .setLabelText("Question help text")
                .setPlaceholderText("The question help text displayed to the applicant")
                .setValue(questionForm.getQuestionText())
                .getContainer())
        .with(formQuestionTypeSelect(QuestionType.valueOf(questionForm.getQuestionType())))
        .condWith(
            questionForm.getQuestionType().equals("TEXT"),
            FieldWithLabel.number()
                .setId("text-min-length")
                .setLabelText("Minimum length (optional)")
                .setValue(questionForm.getTextMinLength())
                .getContainer())
        .condWith(
            questionForm.getQuestionType().equals("TEXT"),
            FieldWithLabel.number()
                .setId("text-max-length")
                .setLabelText("Maximum length (optional)")
                .setValue(questionForm.getTextMaxLength())
                .getContainer());

    // TODO: the condWith condition doesn't actually update live - it just takes the snapshot of
    //  the question type at the time of rendering.

    return formTag;
  }

  private DomContent formQuestionTypeSelect(QuestionType selectedType) {
    ImmutableList<SimpleEntry<String, String>> options =
        Arrays.stream(QuestionType.values())
            .map(item -> new SimpleEntry<>(item.toString(), item.name()))
            .collect(ImmutableList.toImmutableList());

    return new SelectWithLabel()
        .setId("questionType")
        .setLabelText("Question type")
        .setOptions(options)
        .setValue(selectedType.name())
        .getContainer();
  }
}
