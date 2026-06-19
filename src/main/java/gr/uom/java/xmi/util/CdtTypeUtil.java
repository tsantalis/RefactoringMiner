package gr.uom.java.xmi.util;

public final class CdtTypeUtil {
	private CdtTypeUtil() {
	}

	public static String cleanTypeText(String rawType) {
		if(rawType == null) {
			return "";
		}
		return rawType.replaceAll("\\b(static|extern|inline|virtual|explicit|friend|constexpr|consteval|constinit|_Noreturn)\\b", "")
				.trim()
				.replaceAll("\\s+", " ");
	}
}
