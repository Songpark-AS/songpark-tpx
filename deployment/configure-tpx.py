#!/usr/bin/env python3

import argparse
import math
import os
import os.path
import random
import subprocess
import string


AUTHORIZED_KEYS="authorized_keys"
BASE_HW_MAC = "02:0b:2a:11:ff:"
BASE_FAKE_MAC = "00:00:00:00:00:"
BASE_WIREGUARD_IP_RANGE = "10.100.200.1{number}/32"
TEMPLATE_TPX_CONFIG = "config.template.edn"
TEMPLATE_SIP_CONFIG = "sip.template.cfg"
TEMPLATE_WIREGUARD_CONFIG = "wireguard.template.conf"
TEMPLATE_WIREGUARD_SERVER_CONFIG = "wireguard.server.template.conf"
TEMPLATE_ETH1 = "eth1.template"
TPX_CONFIG="config.edn"
SIP_CONFIG="sip.cfg"
ETH1_CONFIG="eth1.teleporter"
WIREGUARD_TELEPORTER_CONFIG="wg0.teleporter.conf"
WIREGUARD_RPI_CONFIG="wg0.rpi.conf"
WIREGUARD_PRIVATE_KEY="wg.private_key.{who}"
WIREGUARD_PUBLIC_KEY="wg.public_key.{who}"
KAMAILIO_COMMANDS="kamailio.commands"
WIREGUARD_SERVER_CONFIG="wireguard.server.config"

def get_random_string(length):
    entries = string.ascii_letters + string.digits
    return ''.join(random.choice(entries) for i in range(length))

def get_number(num):
    return str(num) if num >= 10 else "0" + str(num)

def is_even(num):
    return num % 2 == 0

def get_private_key():
    x = subprocess.run(["wg", "genkey"], stdout=subprocess.PIPE)
    return x.stdout.decode('utf-8').replace('\n', '')

def get_public_key(private_key):
    x = subprocess.run(["wg", "pubkey"], stdout=subprocess.PIPE, input=private_key.encode('utf-8'))
    return x.stdout.decode('utf-8').replace('\n', '')

def get_wireguard_ip(serial_number):
    return "10.100.200." + serial_number + "/32"

def write_config(path, name, config):
    with open(os.path.join(path, name), 'w') as f:
        f.write(config)

def read_config(path, name):
    out = None
    with open(os.path.join(path, name)) as x:
        out = x.read()
    return out

def write_sip_config(template_path, config_path, serial_number):
    serial = get_number(serial_number)
    sip_user = "91{id}".format(id=serial)
    sip_password = get_random_string(32)

    sip_config = read_config(template_path, TEMPLATE_SIP_CONFIG)

    sip_config = sip_config.replace("VAR__SIP_USER", sip_user)
    sip_config = sip_config.replace("VAR__SIP_PASSWORD", sip_password)

    write_config(config_path, SIP_CONFIG, sip_config)

    kamailio_commands = ""
    kamailio_commands += "kamactl add {user} {password}\n".format(user=sip_user, password=sip_password)
    write_config(config_path, KAMAILIO_COMMANDS, kamailio_commands)

def write_tpx_config(template_path, config_path, name, serial_number, logger_level):
    serial = get_number(serial_number)

    tpx_config = read_config(template_path, TEMPLATE_TPX_CONFIG)

    tpx_config = tpx_config.replace("VAR__TELEPORTER_NICKNAME", "{name} 91{serial}".format(name=name, serial=serial))
    tpx_config = tpx_config.replace("VAR__MQTT_USERNAME", "songpark")
    tpx_config = tpx_config.replace("VAR__MQTT_PASSWORD", "fNhWktaTlfDGlH4mbmaW6esOpgExs8wKIOBapDcq")
    tpx_config = tpx_config.replace("VAR__FAKE_MAC", BASE_FAKE_MAC + get_number(serial_number/2))
    tpx_config = tpx_config.replace("VAR__HW_MAC", BASE_HW_MAC + get_number(serial_number))
    tpx_config = tpx_config.replace("VAR__LOGGER_LEVEL", ":" + logger_level)

    write_config(config_path, TPX_CONFIG, tpx_config)

def write_wireguard_config(template_path, config_path, name, serial_number):
    private_key_teleporter = get_private_key()
    public_key_teleporter = get_public_key(private_key_teleporter)
    ip_teleporter = BASE_WIREGUARD_IP_RANGE.format(number=get_number(serial_number))

    private_key_rpi = get_private_key()
    public_key_rpi = get_public_key(private_key_rpi)
    ip_rpi = BASE_WIREGUARD_IP_RANGE.format(number=get_number(serial_number + 1))

    wireguard_teleporter_config = read_config(template_path, TEMPLATE_WIREGUARD_CONFIG)
    wireguard_teleporter_config = wireguard_teleporter_config.replace("VAR__WIREGUARD_PRIVATE_KEY", private_key_teleporter)
    wireguard_teleporter_config = wireguard_teleporter_config.replace("VAR__WIREGUARD_IP", ip_teleporter)

    wireguard_rpi_config = read_config(template_path, TEMPLATE_WIREGUARD_CONFIG)
    wireguard_rpi_config = wireguard_rpi_config.replace("VAR__WIREGUARD_PRIVATE_KEY", private_key_rpi)
    wireguard_rpi_config = wireguard_rpi_config.replace("VAR__WIREGUARD_IP", ip_rpi)

    wireguard_server_config = read_config(template_path, TEMPLATE_WIREGUARD_SERVER_CONFIG)
    wireguard_server_config = wireguard_server_config.replace("VAR__PUBLIC_KEY_TELEPORTER", public_key_teleporter)
    wireguard_server_config = wireguard_server_config.replace("VAR__IP_TELEPORTER", ip_teleporter)
    wireguard_server_config = wireguard_server_config.replace("VAR__PUBLIC_KEY_RPI", public_key_rpi)
    wireguard_server_config = wireguard_server_config.replace("VAR__IP_RPI", ip_rpi)
    wireguard_server_config = wireguard_server_config.replace("VAR__NAME", name)

    write_config(config_path, WIREGUARD_TELEPORTER_CONFIG, wireguard_teleporter_config)
    write_config(config_path, WIREGUARD_RPI_CONFIG, wireguard_rpi_config)
    write_config(config_path, WIREGUARD_SERVER_CONFIG, wireguard_server_config)

    write_config(config_path, WIREGUARD_PRIVATE_KEY.format(who="teleporter"), private_key_teleporter)
    write_config(config_path, WIREGUARD_PRIVATE_KEY.format(who="rpi"), private_key_rpi)
    write_config(config_path, WIREGUARD_PUBLIC_KEY.format(who="teleporter"), public_key_teleporter)
    write_config(config_path, WIREGUARD_PUBLIC_KEY.format(who="rpi"), public_key_rpi)
    
    

def write_eth1_config(template_path, config_path, serial_number):
    hw_mac = BASE_HW_MAC + get_number(serial_number)
    eth1_config = read_config(template_path, TEMPLATE_ETH1)

    eth1_config = eth1_config.replace("VAR__HW_MAC", hw_mac)

    write_config(config_path, ETH1_CONFIG, eth1_config)

def generate_configs(template_path, config_path, logger_level, name, serial_number):
    os.system("mkdir " + config_path)

    if not is_even(serial_number):
        print("Serial number is not even. Use even numbers")
        exit(0)

    write_sip_config(template_path, config_path, serial_number)
    write_tpx_config(template_path, config_path, name, serial_number, logger_level)
    write_eth1_config(template_path, config_path, serial_number)
    write_wireguard_config(template_path, config_path, name, serial_number)

def list_p(x):
    return type(x) == list
    
def copy_file(source, dest):
    s = os.path.join(*source) if list_p(source) else source
    d = os.path.join(*dest) if list_p(dest) else dest
    os.system("cp {source} {dest}".format(source=s, dest=d))

def copy_configs_to_teleporter(template_path, config_path, songpark_path, root_path):
    copy_file([template_path, AUTHORIZED_KEYS],
              [root_path, "root/.ssh", AUTHORIZED_KEYS])
    copy_file([config_path, WIREGUARD_TELEPORTER_CONFIG],
              [root_path, "etc/wireguard", "wg0.conf"])
    copy_file([config_path, WIREGUARD_PUBLIC_KEY.format(who="teleporter")],
              [root_path, "etc/wireguard/keys", "publickey"])
    copy_file([config_path, WIREGUARD_PRIVATE_KEY.format(who="teleporter")],
              [root_path, "etc/wireguard/keys", "privatekey"])
    copy_file([config_path, TPX_CONFIG],
              [songpark_path, "usr/local/etc", TPX_CONFIG])
    copy_file([config_path, SIP_CONFIG],
              [songpark_path, "usr/local/etc", SIP_CONFIG])

def copy_configs_to_rpi(template_path, config_path, rpi_path):
    copy_file([template_path, AUTHORIZED_KEYS],
              [rpi_path, "home/pi/.ssh", AUTHORIZED_KEYS])
    copy_file([config_path, WIREGUARD_RPI_CONFIG],
              [rpi_path, "etc/wireguard", "wg0.conf"])
    copy_file([config_path, WIREGUARD_PUBLIC_KEY.format(who="rpi")],
              [rpi_path, "etc/wireguard/keys", "publickey"])
    copy_file([config_path, WIREGUARD_PRIVATE_KEY.format(who="rpi")],
              [rpi_path, "etc/wireguard/keys", "privatekey"])

def cleanup_configs(config_path):
    os.system("rm -rf " + config_path)

def run_main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--teleporter-name", help="What is the Teleporter to be known as?")
    parser.add_argument("--teleporter-serial", help="Which serial is the Teleporter. Only even numbers", type=int)
    parser.add_argument("--template-path", help="Path to read template configuration files from", default=None)
    parser.add_argument('--config-path', help='Path to write configuration files to', default='config-files')
    parser.add_argument("--logger-level", help="Logger level for logging in TPX", default="info")
    parser.add_argument("--songpark-path", help="Path to the songpark partition of the Teleporter SD card image")
    parser.add_argument("--root-path", help="Path to the root partition of the Teleporter SD card image")
    parser.add_argument("--rpi-path", help="Path to the RPi SD card image")
    parser.add_argument("--action",
                        help="Which action to perform",
                        choices=["generate-configs",
                                 "copy-configs-to-teleporter",
                                 "copy-configs-to-rpi",
                                 "clear-configs"])
    args = parser.parse_args()

    if args.template_path is None:
        print("You need to give a path to the template configuration files")
        exit(0)
    
    if args.action == "generate-configs":
        generate_configs(args.template_path, args.config_path, args.logger_level, args.teleporter_name, args.teleporter_serial)
    elif args.action == "copy-configs-to-teleporter":
        copy_configs_to_teleporter(args.template_path, args.config_path, args.songpark_path, args.root_path)
    elif args.action == "copy-configs-to-rpi":
        copy_configs_to_rpi(args.template_path, args.config_path, args.rpi_path)
    elif args.action == "clear-configs":
        cleanup_configs(args.config_path)
    else:
        print("Choice not supported. Try again")
        print("You supplied the following args")
        print(args)


if __name__ == "__main__":
    run_main()
