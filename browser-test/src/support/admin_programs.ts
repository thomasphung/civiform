import { Page } from 'playwright'
import { readFileSync } from 'fs'

export class AdminPrograms {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminProgramsPage() {
    await this.page.click('nav :text("Programs")');
    await this.expectAdminProgramsPage();
  }

  async expectAdminProgramsPage() {
    expect(await this.page.innerText('h1')).toEqual('All Programs');
  }

  async expectProgramExist(programName: string, description: string) {
    await this.gotoAdminProgramsPage();
    const tableInnerText = await this.page.innerText('main');

    expect(tableInnerText).toContain(programName);
    expect(tableInnerText).toContain(description);
  }

  async addProgram(programName: string, description = 'program description') {
    await this.gotoAdminProgramsPage();
    await this.page.click('#new-program-button');

    await this.page.fill('#program-name-input', programName);
    await this.page.fill('#program-description-textarea', description);
    await this.page.fill('#program-display-name-input', programName);
    await this.page.fill('#program-display-description-textarea', description);

    await this.page.click('text=Save');

    await this.expectAdminProgramsPage();

    await this.expectProgramExist(programName, description);
  }

  selectProgramCard(programName: string, lifecycle: string) {
    return `.cf-admin-program-card:has(:text("${programName}")):has(:text("${lifecycle}"))`;
  }

  selectWithinProgramCard(programName: string, lifecycle: string, selector: string) {
    return this.selectProgramCard(programName, lifecycle) + ' ' + selector;
  }

  async gotoDraftProgramEditPage(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectDraftProgram(programName);
    await this.page.click(this.selectWithinProgramCard(programName, 'DRAFT', ':text("Edit")'));
    await this.expectProgramEditPage(programName);
  }

  async gotoDraftProgramManageTranslationsPage(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectDraftProgram(programName);
    await this.page.click(this.selectWithinProgramCard(programName, 'DRAFT', ':text("Manage Translations")'));
    await this.expectProgramManageTranslationsPage();
  }

  async expectDraftProgram(programName: string) {
    expect(await this.page.innerText(`div.border:has(:text("${programName}"), :text("DRAFT"))`)).not.toContain('New Version');
  }

  async expectActiveProgram(programName: string) {
    expect(await this.page.innerText(this.selectProgramCard(programName, 'ACTIVE'))).toContain('New Version');
  }

  async expectObsoleteProgram(programName: string) {
    expect(await this.page.innerText(this.selectProgramCard(programName, 'OBSOLETE'))).toContain('Applications');
  }

  async expectProgramEditPage(programName: string = '') {
    expect(await this.page.innerText('h1')).toContain(`Edit program: ${programName}`);
  }

  async expectProgramManageTranslationsPage() {
    expect(await this.page.innerText('h1')).toContain('Manage Program Translations');
  }

  async expectProgramBlockEditPage(programName: string = '') {
    expect(await this.page.innerText('id=program-title')).toContain(programName);
    expect(await this.page.innerText('id=block-edit-form')).not.toBeNull();
    // Compare string case insensitively because style may not have been computed.
    expect((await this.page.innerText('[for=block-name-input]')).toUpperCase()).toEqual('BLOCK NAME');
    expect((await this.page.innerText('[for=block-description-textarea]')).toUpperCase()).toEqual('BLOCK DESCRIPTION');
    expect(await this.page.innerText('h1')).toContain('Question bank');
  }

  async editProgramBlock(programName: string, blockDescription = 'block description', questionNames: string[] = []) {
    await this.gotoDraftProgramEditPage(programName);

    await this.page.click('text=Manage Questions');
    await this.expectProgramBlockEditPage(programName);

    await this.page.fill('textarea', blockDescription);
    await this.page.click('#update-block-button');

    for (const questionName of questionNames) {
      await this.page.click(`button:text("${questionName}")`);
    }
  }

  async addProgramBlock(programName: string, blockDescription = 'block description', questionNames: string[] = []) {
    await this.gotoDraftProgramEditPage(programName);

    await this.page.click('text=Manage Questions');
    await this.expectProgramBlockEditPage(programName);

    await this.page.click('#add-block-button');

    await this.page.fill('textarea', blockDescription);
    await this.page.click('#update-block-button');

    for (const questionName of questionNames) {
      await this.page.click(`button:text("${questionName}")`);
    }
  }

  async publishProgram(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectDraftProgram(programName);
    await this.publishAllPrograms();
    await this.expectActiveProgram(programName);
  }

  async publishAllPrograms() {
    await this.page.click(`#publish-programs-button > button`);
  }

  async createNewVersion(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectActiveProgram(programName);
    await this.page.click(this.selectWithinProgramCard(programName, 'ACTIVE', ':text("New Version")'));
    await this.page.click('#program-update-button');
    await this.expectDraftProgram(programName);
  }

  async viewApplications(programName: string) {
    await this.page.click(this.selectWithinProgramCard(programName, 'ACTIVE', ':text("Applications")'));
  }

  selectApplicationCardForApplicant(applicantName: string) {
    return `.cf-admin-application-card:has-text("${applicantName}")`;
  }

  selectWithinApplicationForApplicant(applicantName: string, selector: string) {
    return this.selectApplicationCardForApplicant(applicantName) + ' ' + selector;
  }

  selectApplicationBlock(blockName: string) {
    return `.cf-admin-application-block-card:has-text("${blockName}")`;
  }

  async viewApplicationForApplicant(applicantName: string) {
    await this.page.click(this.selectWithinApplicationForApplicant(applicantName, ':text("View")'));
  }

  async expectApplicationAnswers(blockName: string, questionName: string, answer: string) {
    expect(await this.page.innerText(this.selectApplicationBlock(blockName))).toContain(questionName);
    expect(await this.page.innerText(this.selectApplicationBlock(blockName))).toContain(answer);
  }

  async getCsv() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download all (CSV)"')
    ]);
    const path = await downloadEvent.path();
    if (path === null) {
      throw new Error('download failed');
    }
    return readFileSync(path, 'utf8');
  }

  async addAndPublishProgramWithQuestions(questionNames: string[], programName: string) {
    await this.addProgram(programName);
    await this.editProgramBlock(programName, 'dummy description', questionNames);

    await this.publishProgram(programName);
  }
}
