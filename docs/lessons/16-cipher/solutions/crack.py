# Lesson 16, step 3, in Python: no key, but the message has BYTE in it. Try all 25 keys.
import mine


def decode(text, key):
    plain = ""
    for c in text:
        if "A" <= c <= "Z":
            c = chr((ord(c) - 65 - key) % 26 + 65)
        plain = plain + c
    return plain


secret = mine.challenge()
for key in range(1, 26):
    plain = decode(secret, key)
    if "BYTE" in plain:
        mine.say("The key is " + str(key) + ": " + plain)
        break
