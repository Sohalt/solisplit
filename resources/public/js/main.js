window.onload = () =>{
let names = document.getElementById("names");
function addRow() {
  let row = document.createElement("div");
  row.id = "foo";
  let label = document.createElement("label");
  label.innerText = "name";
  let input = document.createElement("input");
  input.type = "text";
  input.name = "name";
  input.placeholder = "name";
  row.appendChild(label);
  row.appendChild(input);
  input.addEventListener("input",onChange);
  names.appendChild(row)
}
function onChange(e){
  e.target.removeEventListener("input", onChange);
  addRow();
}
addRow();
};
