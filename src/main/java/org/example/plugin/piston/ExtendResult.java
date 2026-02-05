package org.example.plugin.piston;

public enum ExtendResult {
	Empty,
	BreakTarget,
	PistonBlocked,
	PushTarget,
	BreakPiston,
	BreakBlocked, //Will need refactor if we decide to push multiple blocks at the same time (like MC)
	;
}
