from file_handler import FileHandler, AdvancedFileHandler

# Create a handler for our first test file
handler1 = FileHandler('test1.txt')

# Use the colorful display method
print("--- Displaying content in blue ---")
handler1.display_content()

# Combine two files using the '+' operator we fixed
handler2 = FileHandler('test2.txt')
combined_handler = handler1 + handler2

print("\n--- Displaying combined content ---")
combined_handler.display_content()