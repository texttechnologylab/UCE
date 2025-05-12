$(document).ready(function () {
    // TODO adjust height to fit exactly
    // const siteHeight = $(".site-container").height()
    // const headerHeight = $(".header").height()
    // const readerHeight = siteHeight - headerHeight

    console.log("Loading viewerPdf.js for document with type", `${document.getMimeType()}`)

    // TODO check for null
    const pdfBase64 = `${document.getDocumentDataBase64()}`

    const binary = atob(pdfBase64)
    const len = binary.length
    const pdfBytes = new Uint8Array(len)
    for (let i = 0; i < len; i++) {
        pdfBytes[i] = binary.charCodeAt(i)
    }

    const blob = new Blob([pdfBytes], {type: 'application/pdf'})
    const url = URL.createObjectURL(blob)

    let elem = document.getElementById('document-reader-viewer-pdf-iframe')
    // elem.style.height = readerHeight.toString() + "px"
    elem.style.height = "100vh"
    elem.src = url

    console.log("viewerPdf.js loaded")
})