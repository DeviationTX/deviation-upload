RunAsAdmin()
Gui, Show , w320 h300, Window title
Gui, Add, Text, x10 y10 w300 Center,Select which driver to use for your Walkera Transmitter 
Gui, Add, Radio, x10 y70 Checked vDeviation, Install Deviation USB driver (for Deviation-Uploader)
Gui, Add, Radio, vWalkera, Install Walkera USB driver (for DfuSe USB Upgrade)
Gui, Add, Radio, vRemove, Remove all DFU drivers (and don't install any new ones)
Gui, Add, Button, x10 y160 w300 Center vBUTOTON gINSTALL,Install
return

GuiClose: 
ExitApp


INSTALL:
	Gui, submit, NoHide
	; Find all devices
	is64 := Is64bitOS()
	if (is64) {
		_64 := "64"
	}

	; 1st wipe all known DFU devices
	cmd = "%A_WorkingDir%\usbdeview\usbdeview%_64%.exe"
	RunWait %comspec% /c %cmd% /remove_by_pid 0483;df11, , hide

	; 2nd get all inf files that match the pattern
	usbdevs := runcmd("PnPUtil.exe /e")
	infs := get_infs(usbdevs, "STMicroelectronics", "0483", "DF11")
	infs := infs . ";" . get_infs(usbdevs, "libusb 1.0", "0483", "DF11")
	infs := infs . ";" . get_infs(usbdevs, "libusb.info", "0483", "DF11")

	; 3rd Remove all unwanted drivers
	Loop, parse, infs, `;
	{
		if (A_LoopField != "") {
			;MsgBox % A_LoopField
		    runcmd("PnPUtil.exe /d " A_LoopField)
		}
	}
	; 4th install new driver
	if (Deviation=1) {
		InstallWithZadig()
	} else if (Walkera=1) {
		driver = "%A_WorkingDir%\Walkera\STtube.inf"
		msg := runcmd("PnPUtil.exe /i /a " driver)
		MsgBox % msg
	}
	return

get_infs(str, name, vid, pid) {
    lastinf = "none"
	count = 0
    Loop, parse, str, `n, `r  ; Specifying `n prior to `r allows both Windows and Unix files to be parsed.
    {
	    if (RegExMatch(A_LoopField, "Published name.* ([^ ].*\.inf)", inf)) {
		    lastinf := inf1
		} else if (RegExMatch(A_LoopField, "Driver package provider.*:.*" name)) {
			;MsgBox % lastinf
			count += 1
			found%count% := lastinf
		}
	}
	match := "VID_" vid "&PID_" pid

	Loop %count%
	{
		inf := GetCommonFolder() "\inf\" found%A_Index%
		FileRead, infdata, %inf%
		if (InStr(infdata, match)) {
		    final := final . found%A_Index% ";"
		}
	}
    return final
}

InstallWithZadig()
{
	; If zadig is already running, close all occurrences
	while(1) {
		IfWinExist, Zadig
		{
			WinClose, Zadig
		} else {
			break
		}
	}
	;Run zadig (as admin)
	if (A_OSType == "WIN32_NT" && (A_OSVersion == "WIN_XP" || A_OSVersion == "WIN_2003")) {
		;Win XP
		Run %comspec% /c "%A_WorkingDir%\zadig_2.1.2.exe", , hide
	} else {
		Run %comspec% /c "%A_WorkingDir%\zadig_xp_2.1.2.exe", , hide
	}
	;Wait for zadig to start.  close any open windows
	while(1) {
		IfWinExist, Zadig update policy
		{
			WinActivate
			Send {Enter}
		}
		IfWinExist, Zadig
		{
			WinActivate
			break
		}
	}
	;Switch to Custom driver mode
	Send, !dc
	;Wait for mode switch
	while(1) {
		ControlGetText, a, Button3
		if (butTxt = Install Driver) {
			break
		}
	}
	;Fill in driver fields
	Send, STM DFU (WinUSB)
	ControlSetText, Edit3, 0483
	ControlSetText, Edit4, DF11
	Sleep, 200
	;Create driver
	ControlClick, Button3
	;Wait until finished (and User presses 'ok')
	state = 0
	while (state != 2) {
		IfWinExist, Driver Installation
		{
			WinActivate
			state = 1
		} else if (state = 1) {
			state = 2
		}
	}
	;Close zadig
	WinClose, Zadig
}

Is64bitOS() {
    return (A_PtrSize=8)
        || DllCall("IsWow64Process", "ptr", DllCall("GetCurrentProcess"), "int*", isWow64)
        && isWow64
}

runcmd(mycmd) {
	F3        =%temp%\test55.txt
	runwait,%comspec% /c %mycmd% >%F3%,,hide
	FileRead, VAR1, %F3%
	FileDelete,%F3%
	return VAR1
}

GetCommonFolder( CSIDL=0x24 ) { ; www.autohotkey.com/forum/viewtopic.php?p=452955#452955
	VarSetCapacity( Folder,520 ), CS := "" ( A_IsUnicode ? "W":"A" )
	DllCall( "Shell32\SHGetFolderPath" CS, Int,0, Int,CSIDL, Int,0, Int,0, Str,Folder )
Return Folder
}

