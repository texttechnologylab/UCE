# About

We are using the [mkdocs-material package](https://squidfunk.github.io/mkdocs-material/) to create the UCE documentation.

Create a python environment called `venv` *(keep the name "venv" so its git-ingored)*:

```
python -m venv venv
```

Activate it:

- Linux: `source venv/bin/activate`
- Windows: `.\venv\Scripts\activate`

Then install the packages:

```
pip install mkdocs-material mkdocs-glightbox
```

And start the documentation server to start editing:

```
mkdocs serve
```

**To give a short overview:** <br/> The `mkdocs.yml` holds all configurations, themes, styling, navigation routes and other non-markdown content. Everything else can be written in markdown or HTML. Through the `site.css`, you can add custom styling.

**This documentation is automatically built and deployed to the github page whenever someone pushes to `main/master`**.

Refere to this helpful [video tutorial](https://jameswillett.dev/getting-started-with-material-for-mkdocs/#diagrams) for basics about mkdocs-material.