#!/bin/bash

PNG_DIR=./png
GIF_DIR=./gif

SIZE=100

mkdir -p ${GIF_DIR}

for png_file in ${PNG_DIR}/*.png; do
  gif_file="${png_file##*/}"
  gif_file="${GIF_DIR}/${gif_file%*.png}"
  convert ${png_file} -resize ${SIZE}x${SIZE} -background transparent -gravity center -extent ${SIZE}x${SIZE} ${gif_file}.gif
done
