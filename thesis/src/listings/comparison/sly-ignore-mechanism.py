# Fragment of the internal SLY implementation
# Skipping ignored characters without using regex
if text[index] in _ignore:
    index += 1
    continue
