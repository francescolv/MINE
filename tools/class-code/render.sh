#!/bin/sh
# The class code's symbols (ClassCodeScreen): from these drawings to 256-pixel pictures in the
# mod. Needs ImageMagick with SVG support (librsvg). Run it again after changing a drawing.
cd "$(dirname "$0")" || exit 1
out=../../src/main/resources/assets/mine/textures/gui/class_code
mkdir -p "$out"
for svg in *.svg; do
	magick -background none -density 384 "$svg" -resize 256x256 "$out/${svg%.svg}.png"
done
