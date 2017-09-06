/**
 * 
 */
package lib.structure.capr;

/**
 * @author Jan Hoinka
 * Implements the java version of <code>energy_par.h</code> of the 
 * CapR package available at <a href="https://github.com/fukunagatsu/CapR">https://github.com/fukunagatsu/CapR</a>.
 * All intellectual credits of this work goes to the original authors. 
 */
public  final class EnergyPar {

	public final static double GASCONST = 1.98717;  // in [cal/K]
	public final static double K0 = 273.15;
	public final static int INF = 1000000;
	public final static double INFD = 1000000.0;
	public final static int TURN = 3; 
	public final static int MAXLOOP = 30;

	public static int temperature = 37;
	public static double kT = (temperature+K0)*GASCONST;
	public static double lxc37=107.856; /* parameter for logarithmic loop energy extrapolation*/
	
	public static int[][] BP_pair = /* @  A  C  G  U*/
									{{ 0, 0, 0, 0, 0},
									 { 0, 0, 0, 0, 5},
									 { 0, 0, 0, 1, 0},
									 { 0, 0, 2, 0, 3},
									 { 0, 6, 0, 4, 0}};

	public static int[] rtype = {0, 2, 1, 4, 3, 6, 5};

	public static int[][] stack37 =	/*          CG     GC     GU     UG     AU     UA  */
								{ {  INF,   INF,   INF,   INF,   INF,   INF,   INF},
								  {  INF,  -240,  -330,  -210,  -140,  -210,  -210},
								  {  INF,  -330,  -340,  -250,  -150,  -220,  -240},
								  {  INF,  -210,  -250,   130,   -50,  -140,  -130},
								  {  INF,  -140,  -150,   -50,    30,   -60,  -100},
								  {  INF,  -210,  -220,  -140,   -60,  -110,   -90},
								  {  INF,  -210,  -240,  -130,  -100,   -90,  -130}};
	
	public static int[] hairpin37 = { INF, INF, INF, 570, 560, 560, 540, 590, 560, 640, 650,
			       			   660, 670, 678, 686, 694, 701, 707, 713, 719, 725,
			       			   730, 735, 740, 744, 749, 753, 757, 761, 765, 769};
	
	public static int[][][] mismatchH37 =
		{ /* @@ */
		  {{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0}},
		  { /* CG */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   { -90, -150, -150, -140, -180}, /* A@  AA  AC  AG  AU */
		   { -90, -100,  -90, -290,  -80}, /* C@  CA  CC  CG  CU */
		   { -90, -220, -200, -160, -110}, /* G@  GA  GC  GG  GU */
		   { -90, -170, -140, -180, -200}},/* U@  UA  UC  UG  UU */
		  { /* GC */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   { -70, -110, -150, -130, -210}, /* A@  AA  AC  AG  AU */
		   { -70, -110,  -70, -240,  -50}, /* C@  CA  CC  CG  CU */
		   { -70, -240, -290, -140, -120}, /* G@  GA  GC  GG  GU */
		   { -70, -190, -100, -220, -150}},/* U@  UA  UC  UG  UU */
		  { /* GU */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,   20,  -50,  -30,  -30}, /* A@  AA  AC  AG  AU */
		   {   0,  -10,  -20, -150,  -20}, /* C@  CA  CC  CG  CU */
		   {   0,  -90, -110,  -30,    0}, /* G@  GA  GC  GG  GU */
		   {   0,  -30,  -30,  -40, -110}},/* U@  UA  UC  UG  UU */
		  { /* UG */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,  -50,  -30,  -60,  -50}, /* A@  AA  AC  AG  AU */
		   {   0,  -20,  -10, -170,    0}, /* C@  CA  CC  CG  CU */
		   {   0,  -80, -120,  -30,  -70}, /* G@  GA  GC  GG  GU */
		   {   0,  -60,  -10,  -60,  -80}},/* U@  UA  UC  UG  UU */
		  { /* AU */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,  -30,  -50,  -30,  -30}, /* A@  AA  AC  AG  AU */
		   {   0,  -10,  -20, -150,  -20}, /* C@  CA  CC  CG  CU */
		   {   0, -110, -120,  -20,   20}, /* G@  GA  GC  GG  GU */
		   {   0,  -30,  -30,  -60, -110}},/* U@  UA  UC  UG  UU */
		  { /* UA */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,  -50,  -30,  -60,  -50}, /* A@  AA  AC  AG  AU */
		   {   0,  -20,  -10, -120,   -0}, /* C@  CA  CC  CG  CU */
		   {   0, -140, -120,  -70,  -20}, /* G@  GA  GC  GG  GU */
		   {   0,  -30,  -10,  -50,  -80}}/* U@  UA  UC  UG  UU */
		};
	
	public static int[] bulge37 = {
			  INF, 380, 280, 320, 360, 400, 440, 459, 470, 480, 490,
			       500, 510, 519, 527, 534, 541, 548, 554, 560, 565,
				   571, 576, 580, 585, 589, 594, 598, 602, 605, 609};
	
	public static int TerminalAU = 50;

	public static int[] internal_loop37 = {
	  INF, INF, 410, 510, 170, 180, 200, 220, 230, 240, 250,
	       260, 270, 278, 286, 294, 301, 307, 313, 319, 325,
	       330, 335, 340, 345, 349, 353, 357, 361, 365, 369};
	
	public static int[][][] mismatchI37 =
		{ /* @@ */
		  {{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0}},
		  { /* CG */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,    0,    0, -110,    0}, /* A@  AA  AC  AG  AU */
		   {   0,    0,    0,    0,    0}, /* C@  CA  CC  CG  CU */
		   {   0, -110,    0,    0,    0}, /* G@  GA  GC  GG  GU */
		   {   0,    0,    0,    0,  -70}},/* U@  UA  UC  UG  UU */
		  { /* GC */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,    0,    0, -110,    0}, /* A@  AA  AC  AG  AU */
		   {   0,    0,    0,    0,    0}, /* C@  CA  CC  CG  CU */
		   {   0, -110,    0,    0,    0}, /* G@  GA  GC  GG  GU */
		   {   0,    0,    0,    0,  -70}},/* U@  UA  UC  UG  UU */
		  { /* GU */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,   70,   70,  -40,   70}, /* A@  AA  AC  AG  AU */
		   {   0,   70,   70,   70,   70}, /* C@  CA  CC  CG  CU */
		   {   0,  -40,   70,   70,   70}, /* G@  GA  GC  GG  GU */
		   {   0,   70,   70,   70,    0}},/* U@  UA  UC  UG  UU */
		  { /* UG */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,   70,   70,  -40,   70}, /* A@  AA  AC  AG  AU */
		   {   0,   70,   70,   70,   70}, /* C@  CA  CC  CG  CU */
		   {   0,  -40,   70,   70,   70}, /* G@  GA  GC  GG  GU */
		   {   0,   70,   70,   70,    0}},/* U@  UA  UC  UG  UU */
		  { /* AU */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,   70,   70,  -40,   70}, /* A@  AA  AC  AG  AU */
		   {   0,   70,   70,   70,   70}, /* C@  CA  CC  CG  CU */
		   {   0,  -40,   70,   70,   70}, /* G@  GA  GC  GG  GU */
		   {   0,   70,   70,   70,    0}},/* U@  UA  UC  UG  UU */
		  { /* UA */
		   {   0,    0,    0,    0,    0}, /* @@  @A  @C  @G  @U */
		   {   0,   70,   70,  -40,   70}, /* A@  AA  AC  AG  AU */
		   {   0,   70,   70,   70,   70}, /* C@  CA  CC  CG  CU */
		   {   0,  -40,   70,   70,   70}, /* G@  GA  GC  GG  GU */
		   {   0,   70,   70,   70,    0}},/* U@  UA  UC  UG  UU */
		};	
	
	public static int ML_closing37 = 340;
	public static int ML_intern37 = 40;
	
	public static int[][] dangle5_37=
		{/*   @     A     C     G     U   */
		   { INF,  INF,  INF,  INF,  INF}, /* no pair */
		   {   0,  -50,  -30,  -20,  -10}, /* CG  (stacks on C) */
		   {   0,  -20,  -30,   -0,   -0}, /* GC  (stacks on G) */
		   {   0,  -30,  -30,  -40,  -20}, /* GU */
		   {   0,  -30,  -10,  -20,  -20}, /* UG */
		   {   0,  -30,  -30,  -40,  -20}, /* AU */
		   {   0,  -30,  -10,  -20,  -20}, /* UA */
		   {   0,    0,     0,    0,   0}  /*  @ */
		};
	
	/* 3' dangling ends (unpaired base stacks on second paired base */
	public static int[][] dangle3_37=
	{/*   @     A     C     G     U   */
	   { INF,  INF,  INF,  INF,  INF},  /* no pair */
	   { 0, -110,  -40, -130,  -60},  /* CG  (stacks on G) */
	   { 0, -170,  -80, -170, -120},  /* GC */
	   { 0,  -70,  -10,  -70,  -10},  /* GU */
	   { 0,  -80,  -50,  -80,  -60},  /* UG */
	   { 0,  -70,  -10,  -70,  -10},  /* AU */
	   { 0,  -80,  -50,  -80,  -60},  /* UA */
	   {   0,    0,     0,    0,   0}   /*  @ */
	};
	
	public static int ML_BASE37 = 0;

	public static int MAX_NINIO = 300;
	
	public static int F_ninio37 = 50;
}
