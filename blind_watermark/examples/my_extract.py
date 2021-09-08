#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# embed string
import numpy as np
import sys
from blind_watermark import WaterMark

path = sys.argv[1]
bit_length = int(sys.argv[2])

# %% 解水印
bwm1 = WaterMark(password_img=1, password_wm=1)

wm_extract = bwm1.extract(path+'output/embedded.png', wm_shape=bit_length, mode='str')
print(wm_extract)

