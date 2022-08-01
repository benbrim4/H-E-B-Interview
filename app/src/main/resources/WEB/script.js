const imageDiv = document.getElementById("images");

document.forms["uploadForm"].addEventListener("submit", async (event) => {
    event.preventDefault();
    const resp = await fetch(event.target.action, {
      method: "POST",
      body: new FormData(event.target),
    });
    const image = await resp.json();
    addImage(image);
    document.forms["uploadForm"].reset();
  });

getImages();

function getImages() {
    fetch("/images").then((response) => response.json()).then((data) => {
    console.log(data);
    renderImages(data);
});
}

function renderImages(data) {
    removeAllChildNodes(imageDiv);
    for (image in data) {
       addImage(data[image]);
    }
}

function addImage(image) {
    const imageElement = document.createElement('div');
    imageElement.classList.add("imageElement");
    const newImage = document.createElement('img');
    newImage.src = image.imageUrl;
    newImage.classList.add("uploadedImage");
    const label = document.createElement('div');
    label.classList.add("imageLabel");
    label.textContent = image.label;
    const objects = document.createElement('div');
    objects.classList.add("imageObjects");
    objects.textContent = 'objects: ' + image.objects;
    imageElement.appendChild(newImage);
    imageElement.appendChild(label);
    imageElement.appendChild(objects);
    imageDiv.appendChild(imageElement);
}

function removeAllChildNodes(parent) {
    while (parent.firstChild) {
        parent.removeChild(parent.firstChild);
    }
}

function searchForImages() {
    console.log("searching for images");
    console.log(document.getElementById("searchBar"));
    fetch('/images?objects=' + document.getElementById("searchBar").value).then((response) => response.json()).then((data) => {
        renderImages(data);
    });
}