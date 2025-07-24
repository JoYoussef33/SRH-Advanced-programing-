# test_file_handler.py

# We need to import our tools: pytest for testing, and our own classes from the other file.
import pytest
from file_handler import FileHandler, AdvancedFileHandler

# A test function must start with the word 'test_'.
def test_file_reading():
    """Tests if the FileHandler can correctly read the content of a file."""
    # ARRANGE: Set up the test. We create a handler for our test file.
    handler = FileHandler("test1.txt")
    # ACT: Perform the action we want to test. We get the content.
    content = handler.content
    # ASSERT: Check if the result is what we expected.
    # The content should be "Hello World\n". The \n is the invisible newline character.
    assert content == "Hello World\n"

def test_file_concatenation_add_operator():
    """Tests if the '+' operator correctly concatenates two files."""
    # ARRANGE: Create handlers for both test files.
    handler1 = FileHandler("test1.txt")
    handler2 = FileHandler("test2.txt")
    # ACT: "Add" the two handlers together. This should create a new file 'concatenated.txt'.
    result_handler = handler1 + handler2
    # ASSERT: Check if the new file's content is correct.
    expected_content = "Hello World\nI CAN'T BELIEVE IT WORKED!!!!\n"
    assert result_handler.content == expected_content
    # We also check if the new file was created at the correct path.
    assert result_handler.filepath == "concatenated.txt"

def test_inheritance_and_overriding():
    """Tests the child class for its unique functionality."""
    # ARRANGE: Create an object of our 'Advanced' child class.
    advanced_handler = AdvancedFileHandler("test2.txt")
    # ACT & ASSERT: Check its new functionality.
    # The word count of "This is a test\n" should be 5.
    assert advanced_handler.word_count() == 5
    # Check that the overridden __str__ method works as expected.
    assert str(advanced_handler) == "*** AdvancedFileHandler for 'test2.txt' ***"

def test_file_not_found():
    """Tests that the program raises the correct error for a non-existent file."""
    # We use 'pytest.raises' to check that a specific error happens.
    # The code inside the 'with' block SHOULD fail. If it does, the test passes.
    with pytest.raises(FileNotFoundError):
        # This should fail because 'non_existent_file.txt' does not exist.
        FileHandler("non_existent_file.txt")