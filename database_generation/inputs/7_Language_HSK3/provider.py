import os
from lib import u8_utils, DEFINITION_AI_LOCALE

class LanguageHsk3Provider(u8_utils.U8Provider):
    """HSK3-level Chinese definitions parsed statically from ccdict.u8.

    Same .u8 logic as the other definition stages: entries are grouped by
    simplified headword and formatted identically. The file was extracted
    from the legacy AiFields LLM cache and is generated externally from
    now on; update() is a no-op (see U8Provider).
    """
    U8_FILE = os.path.join(os.path.dirname(__file__), "ccdict.u8")
    LANGUAGE = DEFINITION_AI_LOCALE
