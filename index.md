## [EXPERIMENTAL] [ABBA](https://www.youtube.com/watch?v=8haRfsY4-_s) - Aligning Big Brains & Atlases

A [Fiji](https://fiji.sc/) plugin for the registration of thin brain slices to various atlases ([3D mouse Allen Brain atlas](http://atlas.brain-map.org/atlas?atlas=602630314), [Waxholm Space Atlas of the Sprague Dawley Rat Brain](https://www.nitrc.org/projects/whs-sd-atlas), and [BrainGlobe atlases](https://github.com/brainglobe/bg-atlasapi)) + [QuPath](https://qupath.github.io) associated tools.

<video autoplay loop muted style="width: 100%;">
  <source src="https://user-images.githubusercontent.com/20223054/149301605-07b27dd0-4010-4ca4-b415-f5a9acc8963d.mp4" type="video/mp4">
  Your browser does not support the video tag.
</video>

Aligning Big Brains & Atlases or ABBA for short, is a Fiji plugin which allows to register adult mouse brain serial sections to several atlases, in coronal, sagittal and horizontal orientations.

It uses [BigDataViewer](https://imagej.net/plugins/bdv/index) and [BigWarp](https://imagej.net/plugins/bigwarp) for the display and on-the-fly computation of spline-transformed multiresolution images (typical output of Whole Slide Imaging).

It has been developed by the [BioImaging & Optics Platform](https://www.epfl.ch/research/facilities/ptbiop/) at EPFL. This page contains the documentation of ABBA (installation and usage). If you require additional help, please post your question on the [image.sc](https://forum.image.sc) forum  and tag your question / issue with `abba` and `fiji` or `qupath`. If you have already installed ABBA, you can also click on `Help > Ask for help in the forum` from the plugin (some helpful information from your local installation will be included in your post).

If you want to test this plugin, you can download one of the following test dataset: 

#### Dataset 1, provided by Lucie Dixsaut, [Johannes Gräff lab](https://www.epfl.ch/labs/graefflab/), EPFL
One animal, 85 serial sections, 3 fluorescent channels (DAPI - nuclei, FITC - autofluorescence, mCherry - labelled sparse cells)
* [Direct download (8Gb zip file)](https://zenodo.org/record/5018719/files/MouseBrainCoronalSerialSections.zip?download=1) - multiresolution Olympus VSI files  
* [Zenodo repository](https://zenodo.org/record/5018719#.YNNYJEzRYuU) 

#### Dataset 2, provided by Bianca A. Silva, [Johannes Gräff lab](https://www.epfl.ch/labs/graefflab/), EPFL
One animal, 87 serial sections, 2 fluorescent channels (nuclei and autofluorescence)
* [Sample sections  (Zenodo repository, 21 Gb) ](https://doi.org/10.5281/zenodo.4715656) - each section has to be downloaded individually (multiresolution ome.tiff file)
* [Downsampled sections  (GDrive, 0.4 Gb) ](https://drive.google.com/file/d/1OVb860hy-UZSSXa_u9drWiPKEunWT_a7/view?usp=sharing)

## [Workshop slides](https://docs.google.com/presentation/d/1c5yG-5Rhz5WlR4Hf9TNVkjqb6yD6oukza8P6vHGVZMw)
## [Installation](installation.md)
## [Using ABBA](usage.md)
## [Developer documentation (In progress...)]()

<!---
### Markdown

Markdown is a lightweight and easy-to-use syntax for styling your writing. It includes conventions for

```markdown
Syntax highlighted code block

# Header 1
## Header 2
### Header 3

- Bulleted
- List

1. Numbered
2. List

**Bold** and _Italic_ and `Code` text

[Link](url) and ![Image](src)
```

For more details see [GitHub Flavored Markdown](https://guides.github.com/features/mastering-markdown/).

### Jekyll Themes

Your Pages site will use the layout and styles from the Jekyll theme you have selected in your [repository settings](https://github.com/BIOP/ijp-imagetoatlas/settings/pages). The name of this theme is saved in the Jekyll `_config.yml` configuration file.

### Support or Contact

Having trouble with Pages? Check out our [documentation](https://docs.github.com/categories/github-pages-basics/) or [contact support](https://support.github.com/contact) and we’ll help you sort it out.

-->