/** 
 * We're trying to keep the JS pretty mimimal for CiviForm, so we're only using it 
 * where it's necessary to improve the user experience.
 *
 * Appropriate uses include:
 *  - Visual transitions that aren't possible with just CSS.
 *  - Rare instances in which we need to update a page without refreshing.
 *  - TBD
 */

function attachDropdown(elementId: string) {
  const dropdownId = elementId + "-dropdown";
  const element = document.getElementById(elementId);
  const dropdown = document.getElementById(dropdownId);
  if (dropdown && element) {
    // Attach onclick event to element to toggle dropdown visibility.
    element.addEventListener("click", () => toggleElementVisibility(dropdownId));

    // Attach onblur event to page to hide dropdown if it wasn't the clicked element.
    document.addEventListener("click", (e) => maybeHideElement(e, dropdownId, elementId));
  }
}

function toggleElementVisibility(id: string) {
  const element = document.getElementById(id);
  if (element) {
    element.classList.toggle("hidden");
  }
}

function maybeHideElement(e: Event, id: string, parentId: string) {
  if (e.target instanceof Element) {
    const parent = document.getElementById(parentId);
    if (parent && !parent.contains(e.target)) {
      const elementToHide = document.getElementById(id);
      if (elementToHide) {
        elementToHide.classList.add("hidden");
      }
    }
  }
}

/** In admin program block edit form - enabling submit button when form is changed or if not empty */
function changeUpdateBlockButtonState(event: Event) {
  const blockEditForm = document.getElementById("block-edit-form");
  const submitButton = document.getElementById("update-block-button");

  const formNameInput = blockEditForm["block-name-input"];
  const formDescriptionText = blockEditForm["block-description-textarea"];

  if ((formNameInput.value !== formNameInput.defaultValue ||
    formDescriptionText.value !== formDescriptionText.defaultValue) &&
    (formNameInput.value !== "" && formDescriptionText.value !== "")) {
    submitButton.removeAttribute("disabled");
  } else {
    submitButton.setAttribute("disabled", "");
  }
}

/** In the admin question form - add a new option input for each new question answer option. */
function addNewQuestionAnswerOptionForm(event: Event) {
  // Copy the answer template and remove ID and hidden properties.
  const newField = document.getElementById("multi-option-question-answer-template").cloneNode(true) as HTMLElement;
  newField.classList.remove("hidden");
  newField.removeAttribute("id");

  // Register the click event handler for the remove button.
  newField.querySelector("[type=button]").addEventListener("click", removeQuestionOption);

  // Find the add option button and insert the new option input field before it.
  const button = document.getElementById("add-new-option");
  document.getElementById("question-settings").insertBefore(newField, button);
}

/** In the admin question form - remove an answer option input for multi-option questions. */
function removeQuestionOption(event: Event) {

  // Get the parent div, which contains the input field and remove button, and remove it.
  const optionDiv = (event.target as Element).parentNode;
  optionDiv.parentNode.removeChild(optionDiv);

}

/** In the enumerator form - add a new input field for a repeated entity. */
function addNewEnumeratorField(event: Event) {
  // Copy the enuemrator field template
  const newField = document.getElementById("enumerator-field-template").cloneNode(true) as HTMLElement;
  newField.classList.remove("hidden");
  newField.removeAttribute("id");

  // Add the remove enumerator field event listener to the delete button
  newField.querySelector("[type=button]").addEventListener("click", removeEnumeratorField);

  // Add to the end of enumerator-fields div.
  const enumeratorFields = document.getElementById("enumerator-fields");
  enumeratorFields.appendChild(newField);
}

function removeEnumeratorField(event: Event) {
  // Get the parent div, which contains the input field and remove button, and remove it.
  const enumeratorFieldDiv = (event.currentTarget as HTMLElement).parentNode;
  enumeratorFieldDiv.parentNode.removeChild(enumeratorFieldDiv);
}

function init() {
  attachDropdown("create-question-button");

  // Submit button is disabled by default until program block edit form is changed
  const blockEditForm = document.getElementById("block-edit-form");
  if (blockEditForm) {
    blockEditForm.addEventListener("input", changeUpdateBlockButtonState);
  }

  // Configure the button on the admin question form to add more answer options
  const questionOptionButton = document.getElementById("add-new-option");
  if (questionOptionButton) {
    questionOptionButton.addEventListener("click", addNewQuestionAnswerOptionForm);
  }

  // Configure the button on the enumerator question form to add more enumerator field options
  const enumeratorOptionButton = document.getElementById("enumerator-field-add-button");
  if (enumeratorOptionButton) {
    enumeratorOptionButton.addEventListener("click", addNewEnumeratorField);
  }
}
init();
