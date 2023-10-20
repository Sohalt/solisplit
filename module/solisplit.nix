{
  config,
  lib,
  pkgs,
  ...
}: let
  cfg = config.services.solisplit;
in
  with lib; {
    options.services.solisplit = {
      enable = mkEnableOption "solisplit";
      port = mkOption {
        type = types.port;
        default = 8000;
        description = "port to listen on";
      };
    };
    config = mkIf cfg.enable {
      systemd.services.solisplit = {
        script = "${pkgs.solisplit}/bin/solisplit --port ${toString cfg.port}";
      };
    };
  }
