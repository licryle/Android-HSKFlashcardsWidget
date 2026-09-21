import os
from lib import u8_utils

class LanguageFrenchProvider(u8_utils.U8Provider):
    """French definitions parsed statically from CFDICT-next-full.u8.

    Same .u8 logic as the base dictionary stage: entries are grouped by
    simplified headword and formatted identically. The file is generated
    externally; update() is a no-op (see U8Provider).
    """
    U8_FILE = os.path.join(os.path.dirname(__file__), "cfdict-next-full.u8")
    LANGUAGE = "fr"
