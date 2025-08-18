$(document).ready(function () {
    const mimeType = `${document.getMimeType()}`
    console.log("Loading viewerImage.js for document with type", mimeType)

    const imageBase64 = `${document.getDocumentDataBase64()}`

    let elem = document.getElementById('document-reader-viewer-image-img')
    elem.style.height = "100vh"
    elem.src = "data:" + mimeType + ";base64," + imageBase64

    console.log("viewerImage.js loaded")
})