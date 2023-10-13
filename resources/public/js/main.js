window.onload = () =>{
  let names = document.getElementById("names");
  let row = names.children[0];
  let input = row.querySelector("input");
  input.addEventListener("input",onChange);
  let rowTemplate = row.cloneNode(true);
  rowTemplate.querySelector("input").removeAttribute("required");

  function addRow() {
    let row = rowTemplate.cloneNode(true);
    let input = row.querySelector("input");
    input.addEventListener("input",onChange);
    names.appendChild(row)
  }

  function onChange(e){
    e.target.removeEventListener("input", onChange);
    addRow();
  }
};
