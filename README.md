# Solisplit

Solisplit is an application to share costs in a more solidaric way.

See [about](./resources/about.md) for an explanation on how it works.

## Hosted deployment

There is a hosted instance at https://solisplit.soha.lt.

## Self-hosting

The easiest way to self host is using [NixOS](https://nixos.org/) with [flakes](https://nixos.org/manual/nix/stable/command-ref/new-cli/nix3-flake.html).
See `example/flake.nix` for an example deployment.

Alternantively you can also get an executable using `nix build github:sohalt/solisplit`, which you can start any way you like.
