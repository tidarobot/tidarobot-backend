import logging
import random
from datetime import datetime, timedelta

from selenium import webdriver
from selenium.common import TimeoutException
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time


options = Options()
# options.add_argument("--auto-open-devtools-for-tabs")
options.binary_location = "/Applications/Brave Browser.app/Contents/MacOS/Brave Browser"

options.add_argument(
    "user-data-dir=/Users/rownicki/Library/Application Support/BraveSoftware/Brave-Browser"
)
options.add_argument("profile-directory=Default")

service = Service("/Users/rownicki/drivers/chromedriver")


logging.basicConfig(
    filename="log.log",
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)

def login(email, password):
    try:
        email_input = wait.until(
            EC.visibility_of_element_located(
                (By.ID, "signInName")
            )
        )
        email_input.send_keys(email)

        button = wait.until(EC.visibility_of_element_located(
            (By.ID, "continue")
        ))
        time.sleep(random.randint(1,5))
        button.click()

        password_input = wait.until(
            EC.visibility_of_element_located(
                (By.ID, "password")
            )
        )
        time.sleep(random.randint(1, 5))
        password_input.send_keys(password)
        sign_in_button = wait.until(EC.visibility_of_element_located(
                (By.ID, "next")
            ))
        time.sleep(random.randint(1, 5))
        sign_in_button.click()
    except TimeoutException:
        logging.info("Login timed out")

def click_start_if_exists():
    try:
        button = wait.until(
            EC.element_to_be_clickable((
                By.XPATH,
                "//button[contains(normalize-space(), 'Start using Tidaro')]"
            ))
        )
        button.click()
    except TimeoutException:
        logging.info("Button not found – continue")
def click_book_spot():


    book_btn = wait.until(
        EC.element_to_be_clickable((
            By.XPATH,
            "//a[.//div[normalize-space()='Book a spot']]"
        ))
    )

    book_btn.click()
    logging.info("Clicked: Book a spot")

def open_spot_area_dropdown():
    try:
        picker = wait.until(
            EC.presence_of_element_located((By.ID, "parking-spot-zones"))
        )

        container = picker.find_element(By.CLASS_NAME, "dropdown__container")

        main_link = container.find_element(By.CLASS_NAME, "dropdown__main")

        main_link.click()
        logging.info("Dropdown clicked")

    except TimeoutException:
        logging.error("Dropdown not found, continue")

def click_dropdown_option(parking_area):
    try:
        menu_item = wait.until(
            EC.element_to_be_clickable((
                By.XPATH,
                f"//a[contains(@class,'dropdown__menu-item') and normalize-space()='{parking_area}']"
            ))
        )
        menu_item.click()
        logging.info(f"Clicked parking spot: {parking_area}")

    except TimeoutException:
        logging.error(f"Parking spot '{parking_area}' not found, continue...")

def choose_parking_spot_area(parking_area):
    open_spot_area_dropdown()
    click_dropdown_option(parking_area)

def find_parking_day(day_strong, month_span):
    """
    Znajduje div.list__item--between, który zawiera <strong>day_strong</strong> i <span>day_span</span>
    Zwraca WebElement.
    """

    xpath = (
        f"//div[contains(@class,'list__item--between')][.//strong[normalize-space()='{day_strong}'] and .//span[normalize-space()='.{month_span}']]"
    )
    try:
        day_element = wait.until(
            EC.presence_of_element_located((By.XPATH, xpath))
        )
        return day_element

    except TimeoutException:
        logging.error(f"Day {day_strong}.{month_span} not found")
        return None
def get_number_of_spots(day_element):
    free_spots_elem = day_element.find_element(
        By.XPATH,
        ".//span[contains(normalize-space(), 'free spots')]"
    )
    free_spots_text = free_spots_elem.text
    return int(free_spots_text.split(":")[1].strip())

def click_book_any_place(day_element):
    button = day_element.find_element(By.XPATH, ".//button[contains(normalize-space(), 'book any')]")
    button.click()

def click_join_the_waiting_list(day_element):
    button = day_element.find_element(By.XPATH, ".//button[contains(normalize-space(), 'join the waiting list')]")
    button.click()


def book_any_place(day, month):
    day_element = find_parking_day(day, month)
    number_of_spots = get_number_of_spots(day_element)
    if number_of_spots > 0:
        click_book_any_place(day_element)
    else:
        click_join_the_waiting_list(day_element)

def wait_until(hour, minute, second):
    now = datetime.now()
    target = now.replace(hour=hour, minute=minute, second=second, microsecond=0)

    if target < now:
        target += timedelta(days=1)
    wait_seconds = (target - now).total_seconds()
    logging.info(f"Waiting {wait_seconds:.1f} seconds until {hour}:{minute}:{second}.")
    time.sleep(wait_seconds)


if __name__ == "__main__":
    config = {
        "login": "<<BNP_EMAIL>>",
        "password": "<<PASSWORD>>",
        "parking_area": "CIB_Kraków -2",
        "day": "09",
        "month": "05"
    }

    # wait_until(23, 56, 20)

    driver = webdriver.Chrome(service=service, options=options)
    wait = WebDriverWait(driver, 25)
    url = "https://google.com"
    driver.get(url)

    url = "https://share.parkanizer.com"
    driver.get(url)

    login(config['login'], config["password"])
    click_start_if_exists()
    click_book_spot()
    choose_parking_spot_area(config['parking_area'])
    # wait for a certain time
    # wait_until(14, 15, 0)
    driver.refresh()

    book_any_place(config['day'], config['month'])

    time.sleep(30)
