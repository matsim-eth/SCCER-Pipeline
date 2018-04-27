#!/bin/bash


scp /C/Projects/SCCER_project/target/matsim-SCCER-0.1-jar-with-dependencies.jar molloyj@euler.ethz.ch:programs/SCCER.jar
scp -r /P/Projekte/SCCER/switzerland_10pct/ molloyj@euler.ethz.ch:data/switzerland_10pct


tar czf - /C/Projects/SCCER_project/output_gc/csv/*  | ssh molloyj@euler.ethz.ch "cd data/ && tar xvzf -"

bsub -n 36 -R fullnode -W 10:00 -B < ~/programs/sccer_large.lsf

find . -type f | wc -l #get number of created files in directory
