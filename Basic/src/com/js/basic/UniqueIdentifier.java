package com.js.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for testing and development purposes that assigns a unique identifier
 * to an object. Identifiers are guaranteed to be unique, and the class is
 * threadsafe.
 * 
 */
public class UniqueIdentifier {

	/**
	 * Given an object, return a unique human-readable string derived from its
	 * hashcode
	 * 
	 * @param object
	 * @return
	 */
	public static String nameFor(Object object) {
		if (object == null)
			return "<null>";
		String name;
		int hashCode = object.hashCode();
		synchronized (UniqueIdentifier.class) {
			if (objectNames.isEmpty()) {
				buildNameMap();
			}
			name = objectNames.get(hashCode);
			if (name == null) {
				int index = objectNames.size();
				PrefixEntry entry = namePrefixes.get(index
						% namePrefixes.size());
				int cell = entry.allocMember();
				name = entry.mName;
				if (cell != 0)
					name = name + cell;
				objectNames.put(hashCode, name);
			}
		}
		return name;
	}

	public int getId() {
		if (mId == 0)
			mId = allocIdentifier();
		return mId;
	}

	@Override
	public String toString() {
		int id = getId();
		return nameFor(id);
	}

	private static int allocIdentifier() {
		int id;
		synchronized (UniqueIdentifier.class) {
			previousIdentifier++;
			id = previousIdentifier;
		}
		return id;
	}

	private static final String THREE_LETTER_WORDS = ""//
			+ //
			"ASK BOW COB DOE EAT FAY GAB HOG IVY JOB KOI LIL MAC NAG OVA PAL RIG SAG TAR URN "
			+ "VOX WAN YAH ZIT ACE BUT CON DAP ELF FRO GAR HAT ISM JAM KEY LIP MOO NAN OWL PAY "
			+ "RAG SAT TAB UGH VAN WET YAK ZAG AMP BIB CAW DIP EEL FEN GET HUB ION JOT KID LEG "
			+ "MOW NOB OAR PAN RAN SAP TAD USE VIA WHY YUK ZOO ADD BUY CUD DUG EAR FIT GYM HEH "
			+ "ILL JUT KEN LIB MAT NAH OUT PIX RUB SAL TAG UMP VIM WEE YAW ZIG APE BOA COW DEB "
			+ "EGG FOG GUN HMM ILK JAY KEG LOW MET NIB OOH PAX RAD SAW TUB VET WAX YEP ZIP AIR "
			+ "BAH COY DEN EON FOR GUY HOE IMP JOE KIT LOU MID NIP ODD PIN ROB SAM TSK VAT WHO "
			+ "YUP ZEN ASS BAY COT DIE ERE FIE GAP HOY IRK JEW KAY LAY MAR NTH OIL POW ROE SIP "
			+ "TAW VEX WAS YEW ZAP ARK BOX CUR DON ELM FEW GOO HUG INK JOG KIP LET MEW NON ORE "
			+ "PAP RYE SET TEE VIE WAY YOU ZOE ARC BOP COD DIG EWE FOP GOT HEM IRE JIG KIN LOB "
			+ "MOP NIL OPT PAD ROT SLY THO VOW WIT YAM ADZ BET COG DUB EGO FEM GAS HAY INN JAG "
			+ "KAT LEI MAN NOD OLE PIC RIB SEE TOM VEG WON YON ADO BUD CUT DEW ELS FAR GUS HEY "
			+ "ICE JET LAD MUG NAY OWN PIG RUN SIS TOP WRY YIN ALE BOD CAM DID EVE FAX GAG HID "
			+ "ITS JAB LIT MOD NAP OAK PEA RAH SHH TOY WAD YET ASH BIO CAR DOC EKE FOX GIN HIS "
			+ "IDA JUG LAX MUM NAB OAT PIP RID SPY TAM WEB YIP AVE BOG CAL DAM ETC FED GUT HER "
			+ "ICY JAR LYE MOM NEW ONE POI RIM SUE THY WOW YEN AID BIN COP DUH ELK FIN GOD HOP "
			+ "JOY LAW MEN NOT OAF PEW ROM SUN TEA WAG YAP AMY BOB CAN DAY ERA FLY GOB HIT JAW "
			+ "LOT MUD NIX OWE PAT ROW SOT TIL WOO YUM ASP BAM COO DOW ELL FEE GUM HAG JIB LAB "
			+ "MAY NED OFT PUS ROD SHE TIS WOK YEH AUK BEA CEL DOG EBB FAT GAL HAW LAG MAP NOR "
			+ "OFF PIE RAW SOD TED WAR YEA APT BAT CAD DIM EZO FUN GIG HOB LUX MRS NOW OHM PUB "
			+ "RED SHY TON WIN YES AND BUS CAY DAB EMU FLU GAT HOT LOP MAD NIT OLD PUT RIP SON "
			+ "TOG WOE AGO BED CAT DRY ERG FUR GAY HEW LOX MIX NET ODE PRY RAM SEA TAX WIZ ART "
			+ "BAN CAP DUE EYE FAN GEL HUT LOG MOB OUR PAR RUM SAX TIP WED ATM BOT CUB DUD END "
			+ "FOE GEM HIM LIE MAX PLY RUT SEX TUX WIG AGE BAD CRY DYE FIR GEE HAM LEE MAW PAM "
			+ "RAP SKA TOW ANY BIG CAB DOT FOB GAD HEX LID PET RUG SOX TAN ALL BRA CUP DIN FAD "
			+ "GAM HUE LUG POT RON SEW TAP ARM BUB CUE DUN FIG GNU HOW LAP PUG REP SOB TOE AFT "
			+ "BUM DOP FIB HOD LED PEN RAY SOP TEN ANT BOO DUO FIX HAS LAM POP RAT SIX TRY ARE "
			+ "BEG DAD FRY HIE PUP REX SOL TUT AIM BUG FEZ HUH PEG RUE SAN TIT AVA BEE HIP POX "
			+ "REF SIT TIC ANN BAG HEN PER SIN TAT AXE BIZ HUM PUN STY TWO AWE BAR HAH POD SUP "
			+ "TOT AHA BID PIT SAK TIN ATE BYE PAW SPA TIE AIL BIT PEP SOY TUG AYE BUN PRO SUB "
			+ "THE AWL BOY SKI TOO ACT BON SKY SAY SUM SIR SOW SAD ";

	private static void buildNameMap() {
		for (int i = 0; i < THREE_LETTER_WORDS.length(); i += 4) {
			String prefix = THREE_LETTER_WORDS.substring(i, i + 3);
			namePrefixes.add(new PrefixEntry(prefix));
		}
	}

	private static class PrefixEntry {
		PrefixEntry(String name) {
			mName = name;
		}

		int allocMember() {
			return population++;
		}

		String mName;
		int population;
	}

	private static List<PrefixEntry> namePrefixes = new ArrayList();
	private static Map<Integer, String> objectNames = new HashMap();
	private static int previousIdentifier;

	private int mId;
}
