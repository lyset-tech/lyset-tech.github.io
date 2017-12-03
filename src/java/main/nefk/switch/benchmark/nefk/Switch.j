.class public nefk/Switch
.super java/lang/Object

.method public <init>()V
   aload_0
   invokenonvirtual java/lang/Object/<init>()V
   return
.end method

.method public static lookupSwitch(I)I
    .limit stack 1

    iload_0
    lookupswitch
      1 : One
      2 : Two
      3 : Three
      4 : Four
      5 : Five
      6 : Six
      7 : Seven
      default : Other

One:
    bipush 11
    ireturn
Two:
    bipush 22
    ireturn
Three:
    bipush 33
    ireturn
Four:
    bipush 44
    ireturn
Five:
    bipush 55
    ireturn
Six:
    bipush 66
    ireturn
Seven:
    bipush 77
    ireturn
Other:
    bipush -1
    ireturn
.end method

.method public static tableSwitch(I)I
    .limit stack 1

    iload_0
    tableswitch 1
      One
      Two
      Three
      Four
      Five
      Six
      Seven
      default : Other

One: 
    bipush 11
    ireturn
Two:
    bipush 22
    ireturn
Three:
    bipush 33
    ireturn
Four:
    bipush 44
    ireturn
Five:
    bipush 55
    ireturn
Six:
    bipush 66
    ireturn
Seven:
    bipush 77
    ireturn
Other:
    bipush -1
    ireturn
.end method
