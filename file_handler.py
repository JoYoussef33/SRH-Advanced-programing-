# file_handler.py

# This is our custom decorator. A decorator is like a wrapper you put around a function
# to give it extra abilities without changing the function's code.
# This one takes an argument, 'color', to know what color to use.
def colorize(color: str):
    """A decorator to print text in a specific color using ANSI codes."""
    # ANSI escape codes are special sequences of characters that tell the terminal
    # to change text color, style, etc.
    colors = {
        "red": "\033[91m",
        "green": "\033[92m",
        "blue": "\033[94m",
        "end": "\033[0m", # This code resets the color back to default
    }

    # This is the actual decorator function that will wrap our method.
    def decorator(func):
        # This is the new function that has the "extra power".
        # It will be called instead of the original function.
        def wrapper(*args, **kwargs):
            # Get the text from the original function.
            text_to_print = func(*args, **kwargs)
            # Get the correct color code from our dictionary.
            color_code = colors.get(color, "")
            # Get the code to end the color.
            end_code = colors["end"]
            # Print the text wrapped in color codes, which changes its color in the terminal.
            print(f"{color_code}{text_to_print}{end_code}")
        # The decorator returns the new, more powerful function.
        return wrapper
    # The outer function returns the decorator itself.
    return decorator


# This is the blueprint for our file-handling object.
# It's called a 'class'.
class FileHandler:
    """A class to handle file operations like reading and concatenation.""" # This is a Docstring!

    # The __init__ method is the constructor. It's called automatically
    # when you create a new FileHandler object. e.g., my_file = FileHandler('file.txt')
    def __init__(self, filepath: str):
        # We save the path to the file inside the object.
        self.filepath = filepath
        # We check if the file actually exists using our static method.
        if not FileHandler.file_exists(self.filepath):
            # If the file doesn't exist, we raise an error to stop the program.
            raise FileNotFoundError(f"The file {self.filepath} was not found.")

    # This is a generator method. Instead of reading the whole file into memory at once,
    # it reads it line by line. This is much more efficient for very large files.
    def _read_lines_generator(self):
        """A generator that yields lines from the file one by one."""
        # 'with open(...)' is the safe way to open files in Python.
        # It automatically closes the file for you, even if errors occur.
        with open(self.filepath, 'r') as f:
            # We loop through each line in the file.
            for line in f:
                # 'yield' is like 'return', but it doesn't stop the function.
                # It just pauses and gives back one line at a time.
                yield line

    # This is a 'property'. It lets us call a method like it's a simple variable.
    # So we can type 'my_file.content' instead of 'my_file.get_content()'.
    @property
    def content(self) -> str:
        """Property to get the full content of the file as a single string."""
        # This is a list comprehension. It's a short, clean way to create a list.
        # We use our generator to get each line and build a list of lines.
        lines = [line for line in self._read_lines_generator()]
        # ''.join(lines) combines all the lines in the list into one single piece of text.
        return ''.join(lines)
    
    # This is the 'setter' for our 'content' property. It allows us to
    # change the file's content by doing 'my_file.content = "new text"'.
    @content.setter
    def content(self, new_content: str):
        """Allows setting the file's content, overwriting the file."""
        # We open the file in 'write' mode ('w'), which erases the old content.
        with open(self.filepath, 'w') as f:
            # We write the new content to the file.
            f.write(new_content)

    # This is a special "dunder" (double underscore) method. It tells Python
    # what to show when you try to 'print()' your object.
    def __str__(self):
        """Returns a user-friendly string representation of the object."""
        # It will print a nice message instead of something cryptic like '<FileHandler object at ...>'.
        return f"FileHandler object for file: '{self.filepath}'"

# This is another special method. It defines what the '+' operator does.
    # It allows us to "add" two FileHandler objects together.
    def __add__(self, other):
        """Concatenates the content of this file with another file."""
        # 'isinstance' checks if the 'other' object is also a FileHandler.
        if not isinstance(other, FileHandler):
            # If you try to add a FileHandler to a number, for example, it will raise an error.
            return NotImplemented
        
        # Get the content from both files.
        combined_content = self.content + other.content
        new_filepath = "concatenated.txt"
        
        # --- Start of Fix ---
        # First, we must create and write the content to the new file.
        with open(new_filepath, 'w') as f:
            f.write(combined_content)
        
        # Now that the file exists, we can create a FileHandler for it.
        return FileHandler(new_filepath)
        # --- End of Fix ---

    # A class method is a method that belongs to the class itself, not to a
    # specific object. You call it on the class: FileHandler.concat_files(...)
    @classmethod
    def concat_files(cls, *filepaths, output_path="combined.txt"):
        """
        A class method to concatenate any number of files into a single output file.
        """
        # We create a list to hold all the content.
        all_content = []
        # We loop through all the file paths given to the method.
        for path in filepaths:
            # For each path, we create a temporary FileHandler object.
            # 'cls' here refers to the class itself, which is FileHandler.
            handler = cls(path)
            # We add its content to our list.
            all_content.append(handler.content)
        
        # We join all the content together with a newline in between.
        final_content = "\n".join(all_content)
        # We open the output file in write mode.
        with open(output_path, 'w') as f:
            # We write the final, combined content to the file.
            f.write(final_content)
        # This method returns a new FileHandler object for the combined file.
        return cls(output_path)

    # A static method doesn't know anything about the class or the object.
    # It's just a regular function that is grouped with the class for organization.
    @staticmethod
    def file_exists(filepath: str) -> bool:
        """A static method to check if a file exists at a given path."""
        try:
            # We try to open the file.
            with open(filepath, 'r'):
                pass # If it opens successfully, we do nothing.
            # If we get here, the file exists.
            return True
        except FileNotFoundError:
            # If we get a FileNotFoundError, it means the file doesn't exist.
            return False

    # This is the method we will apply our custom decorator to.
    @colorize("red") # We use our decorator here, asking for blue text.
    def display_content(self):
        """Displays the content of the file. The decorator will color it."""
        return self.content


# This is the child class that inherits from FileHandler.
# It gets all the powers of FileHandler for free, and we can add more!
class AdvancedFileHandler(FileHandler):
    """
    An advanced file handler that inherits from FileHandler and adds
    extra functionality like word counting.
    """

    # We are overriding the __str__ method from the parent.
    # This child class will now have a different print output.
    def __str__(self):
        """Overrides the parent's __str__ method for a fancier output."""
        return f"*** AdvancedFileHandler for '{self.filepath}' ***"

    # This is a brand new method that only the AdvancedFileHandler has.
    def word_count(self) -> int:
        """A new method to count the words in the file."""
        # We get the content of the file.
        text = self.content
        # We split the text into a list of words.
        words = text.split()
        # We return the length of the list, which is the word count.
        return len(words)