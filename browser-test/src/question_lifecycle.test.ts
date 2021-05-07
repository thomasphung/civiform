import { startSession, loginAsAdmin, AdminQuestions, AdminPrograms, endSession } from './support'

describe('normal question lifecycle', () => {
  it('create, update, publish, create a new version, and update all questions', async () => {
    const { browser, page } = await startSession();
    page.setDefaultTimeout(4000);

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    const questions = await adminQuestions.addAllNonSingleBlockQuestionTypes('qlc-');
    const singleBlockQuestions = await adminQuestions.addAllSingleBlockQuestionTypes('qlc-');

    await adminQuestions.updateAllQuestions(questions);

    const programName = 'program for question lifecycle';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'qlc program description', questions);
    for (const singleBlockQuestion of singleBlockQuestions) {
      await adminPrograms.addProgramBlock(programName, 'single-block question', [singleBlockQuestion]);
    }
    await adminPrograms.publishProgram(programName);

    await adminQuestions.expectActiveQuestions(questions);

    await adminQuestions.createNewVersionForQuestions(questions)

    await adminQuestions.updateAllQuestions(questions);

    await adminPrograms.publishProgram(programName);

    await adminPrograms.createNewVersion(programName);

    await adminPrograms.publishProgram(programName);

    await adminQuestions.expectActiveQuestions(questions);

    await endSession(browser);
  })
})
