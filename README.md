# Image List Browser (ILB)

The Image (List) Browser (referred to as ILB) is a Java-based tool for browsing and annotating local images. It was developed to allow creation of ground truth datasets for research on face matching algorithms. 

## Features

- View images from a directory in a gallery format (including larger directories with 10,000+ images rapidly).
- Filter images with the aid of a list file (i.e. only view images that are on the list).
- Add labeled boundaries (e.g. face, eye, nose bounds) and assign attributes (e.g. male/female, wearing glasses/sunglasses, roll/pitch/yaw) and store them in a list file.
- Sort images into sets based either on whole images, or on specific image annotations.
- Make calls to an optional, separate in-house developed FaceMatch library to automatically detect and match faces.

## Installation

The ILB is known to work with Java 8. (If your JRE does not include JavaFX, install it separately.) It requires jimObjModelImporterJFX.jar, a third-party Java library used for importing .obj 3D models into JavaFX for displaying a 3D head, which we provide here. If automatic face detection and face matching are desired, the following additional items are required: A compiled FaceMatch library (C++), a compiled FaceMatchJavaInterface library (C++), FaceMatch Models, and OpenCV 3.2. The FaceMatch components can be downloaded from [here](https://github.com/lhncbc/FaceMatch-1).

To build it, compile the Java source files and place them in ILB.jar in the main directory. Run the ILB as follows:
```
java –jar ILB.jar:jimObjModelImporterJFX.jar ilb.Main 
```
**Usage**

Refer to Manual/ImageAnnotationGuide.docx for instructions on how to use the ILB, and to Manual/ListFileSyntax.docx document for the list file format.

## License

Informational Notice:

This software, the “Image List Browser,” was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, an agency of the Department of Health and Human Services, United States Government.

The license of this software is an open-source BSD-like license.  It allows use in both commercial and non-commercial products.

The license does not supersede any applicable United States law.

The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation (FAR) 48 C.F.R. Part 52.227-14, Rights in Data—General.
The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and non-commercial products.

LICENSE:

Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership, as provided by Federal law.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

- Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

- The names, trademarks, and service marks of the National Library of Medicine, the National Institutes of Health, and the names of any of the software developers shall not be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITEDTO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
