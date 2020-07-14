# MiBLE
BLE android app which pull data directly from Mi Scale

## Data Format in JSON
```
{"Year:Month:Date:Hour:Min:Sec":{"Object":"", 
                                 "Unit":"", 
                                 "Weight":""}

Object: 1(Object on scale)
        0(Object removed)
Unit:   1(lb)
        0(kg)
```
