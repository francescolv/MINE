# Lesson 16, step 1, in Python: every letter was moved 3 places forward: move it back.
import mine


def decode(text, key):
    plain = ""
    for c in text:
        if "A" <= c <= "Z":
            c = chr((ord(c) - 65 - key) % 26 + 65)  # A is 65
        plain = plain + c
    return plain


secret = mine.challenge()
mine.say(decode(secret, 3))
