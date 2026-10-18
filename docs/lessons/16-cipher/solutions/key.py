# Lesson 16, step 2, in Python: now MINE gives the key too.
import mine


def decode(text, key):
    plain = ""
    for c in text:
        if "A" <= c <= "Z":
            c = chr((ord(c) - 65 - key) % 26 + 65)
        plain = plain + c
    return plain


secret, key = mine.challenge()
mine.say(decode(secret, key))
