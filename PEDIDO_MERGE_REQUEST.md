<!-- Título do merge request: New app: Painel Financeiro -->
<!-- Antes de enviar: anexa 2 ou 3 capturas de ecrã e confirma cada [x] abaixo. -->

## Abstract for new app inclusion

### Please introduce the functions and usage of your app in summary, and also attach some screenshots.

Painel Financeiro is a simple dashboard to follow a monthly investment plan. The user logs each
investment with one tap ("Já investi"), the app updates the totals, and goals can be set to track
progress. All data is stored locally on the device (Room database). No ads, no tracking.

(screenshots attached)

### Please elaborate why you wrote this app. Please highlight the unique functions it provides over other similar apps in F-Droid.

<!-- Ajusta este parágrafo com as tuas razões. -->
I wrote it as an open-source academic project. Unlike general expense trackers, it focuses on
recurring investments and goals, with a minimal interface and no account required.

## Checklist

### Policy

* [x] The app complies with the [inclusion policy](https://f-droid.org/docs/Inclusion_Policy).
* [x] The original app author has been notified (and does not oppose the inclusion). I am the author.
* [x] The upstream app source code repo contains the app metadata in a Fastlane folder structure, including the `en-US` locale.

### Docs

* [x] I have read the guide, the templates, the Build Metadata Reference and the Quick Start Guide.

### Merge Request Setup

* [x] The title follows the "New app: app name" format.
* [x] My fdroiddata fork is public and the branch is not protected.
* [x] There are no related fdroiddata or RFP issues.
* [x] Only one app in this MR.

### Metadata

* [x] Metadata is in `metadata/pt.painelfinanceiro.app.yml`, valid YAML with LF line endings (checked with `fdroid lint` and `fdroid rewritemeta`).
* [x] No summary/description/changelog/images were added here; they are in the upstream repo.
* [x] Releases are tagged and auto update is enabled.
* [x] There is an issue tracker and an AuthorName.
* [x] No external repos (no submodules or srclibs).
* [ ] Reproducible Builds: not enabled for this first release; I am fine with the APK being signed with the F-Droid key.
* [x] No native code, so no ABI split is needed.
* [x] Only the latest version is in the metadata, with no disabled versions.
* [x] The `commit` field is the full hash of the release tag.

### Pipeline

* [ ] All pipelines pass. <!-- marca depois de confirmares no GitLab -->
* [ ] All warnings and errors in the Reports tab are fixed or explained.

---------------------

/label ~"New App"
