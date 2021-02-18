package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;

import com.google.inject.Guice;
import com.google.inject.util.Modules;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.test.Helpers;
import repository.ProgramRepository;
import repository.WithPostgresContainer;
import play.mvc.Result;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;
// import com.google.inject.testing.fieldbinder.Bind;
// import com.google.inject.testing.fieldbinder.BoundFieldModule;
import services.program.ProgramService;
import services.program.test.FakeProgramService;

public class AdminProgramControllerTest extends WithPostgresContainer {

  //  @Bind(to = ProgramService.class)
  //  private final FakeProgramService fakeService = new FakeProgramService();

  private AdminProgramController controller;
  private ProgramRepository repo;

  //  @Before
  //  public void setup() {
  //    controller =
  //
  // Guice.createInjector(BoundFieldModule.of(this)).getInstance(AdminProgramController.class);
  //  }

  @Before
  public void setup() {

    // TODO(cdanzi): NOte to self - test each endpoint
    controller = app.injector().instanceOf(AdminProgramController.class);
    repo = app.injector().instanceOf(ProgramRepository.class);
  }

  @Test
  public void listWithNoPrograms_returnsExpectedHtml() {
    Result result = controller.list();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType().get()).isEqualTo("text/html");
    assertThat(result.charset().get()).isEqualTo("utf-8");
    assertThat(contentAsString(result)).contains("Programs");
  }

  @Test
  public void newOne_returnsExpectedForm() {
    RequestBuilder requestBuilder = Helpers.fakeRequest();
    requestBuilder = addCSRFToken(requestBuilder);

    Result result = controller.newOne(requestBuilder.build());

    assertThat(result.status()).isEqualTo(OK);
  }
}
