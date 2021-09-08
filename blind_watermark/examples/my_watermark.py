#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# embed string
import sys
import numpy as np


from blind_watermark import WaterMark

path = sys.argv[1]
wm = sys.argv[2]
bwm1 = WaterMark(password_img=1, password_wm=1)

bwm1.read_img(path+"pic/ori_img.jpg")

bwm1.read_wm(wm, mode='str')
bwm1.embed(str(path)+'output/embedded.jpg')
len_wm = len(bwm1.wm_bit)
print('{len_wm}'.format(len_wm=len_wm))